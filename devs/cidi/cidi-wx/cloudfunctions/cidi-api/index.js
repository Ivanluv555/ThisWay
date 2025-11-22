// cloudfunctions/cidi-api/index.js
const cloud = require('wx-server-sdk')
cloud.init({ env: cloud.DYNAMIC_CURRENT_ENV })

const db = cloud.database()
const _ = db.command
const $ = db.command.aggregate

exports.main = async (event, context) => {
  const wxContext = cloud.getWXContext()
  const openid = wxContext.OPENID
  const { action, payload } = event 

  // 简单的路由分发
  switch (action) {

    // ============================================================
    //  1. 发布相关
    // ============================================================
    
    // 发布帖子
    case 'createPost':
      return await db.collection('cidi_posts').add({
        data: {
          ...payload, // 包含 title, content, images
          _openid: openid, 
          createTime: db.serverDate(),
          viewCount: 0,
          likeCount: 0,
          commentCount: 0,
          // 存储发布者简要信息
          author: payload.userInfo.username || '微信用户',
          authorAvatar: payload.userInfo.avatarUrl || 'https://ui-avatars.com/api/?name=User&background=random'
        }
      })

    // ============================================================
    //  2. 读取列表相关
    // ============================================================

    // 获取首页列表 (支持分页)
    case 'getHomeList':
      return await db.collection('cidi_posts')
        .orderBy('createTime', 'desc') // 按发布时间倒序
        .skip(payload.offset || 0)
        .limit(payload.limit || 20)
        .get()

    // 获取我的收藏列表 (多表联查逻辑)
    case 'getUserCollections':
      // 1. 先查动作表，找到我收藏的所有 postId
      const myCollects = await db.collection('cidi_actions')
        .where({
          _openid: openid, 
          type: 'collect'
        })
        .orderBy('createTime', 'desc')
        .get()

      if (myCollects.data.length === 0) {
        return { code: 0, data: [] }
      }

      // 2. 提取 ID 数组
      const postIds = myCollects.data.map(item => item.postId)

      // 3. 批量查询文章表
      const posts = await db.collection('cidi_posts')
        .where({
          _id: _.in(postIds)
        })
        .get()

      return { code: 0, data: posts.data }

    // ============================================================
    //  3. 详情与互动相关
    // ============================================================

    // 获取文章详情 (包含我的点赞/收藏状态)
    case 'getArticleDetail':
      const post = await db.collection('cidi_posts').doc(payload.id).get()
      
      // 并行查询状态
      const isLiked = await db.collection('cidi_actions').where({
        postId: payload.id, _openid: openid, type: 'like'
      }).count()
      
      const isCollected = await db.collection('cidi_actions').where({
        postId: payload.id, _openid: openid, type: 'collect'
      }).count()

      return {
        post: post.data,
        isLiked: isLiked.total > 0,
        isCollected: isCollected.total > 0
      }

    // 执行点赞/收藏 (事务处理：记录表加减 + 计数器加减)
    case 'toggleAction':
      const { postId, type } = payload // type: 'like' | 'collect'
      
      const exist = await db.collection('cidi_actions').where({
        postId, _openid: openid, type
      }).get()

      if (exist.data.length > 0) {
        // [取消操作]
        await db.collection('cidi_actions').doc(exist.data[0]._id).remove()
        // 只有点赞才更新文章计数，收藏通常不公开计数(看需求)
        if(type === 'like') {
           await db.collection('cidi_posts').doc(postId).update({ data: { likeCount: _.inc(-1) } })
        }
        return { status: false }
      } else {
        // [执行操作]
        await db.collection('cidi_actions').add({
          data: { postId, type, createTime: db.serverDate() }
        })
        if(type === 'like') {
           await db.collection('cidi_posts').doc(postId).update({ data: { likeCount: _.inc(1) } })
        }
        return { status: true }
      }

    // ============================================================
    //  4. 评论相关
    // ============================================================

    // 发表评论
    case 'addComment':
      await db.collection('cidi_comments').add({
        data: {
          postId: payload.postId,
          content: payload.content,
          userInfo: payload.userInfo,
          createTime: db.serverDate()
        }
      })
      // 帖子评论数 +1
      await db.collection('cidi_posts').doc(payload.postId).update({
        data: { commentCount: _.inc(1) }
      })
      return { success: true }

    // 获取评论列表
    case 'getComments':
      return await db.collection('cidi_comments')
        .where({ postId: payload.postId })
        .orderBy('createTime', 'desc')
        .get()

    // ============================================================
    //  5. 搜索功能 (数据库版)
    // ============================================================
    case 'search':
      const { keyword } = payload
      if (!keyword) return { code: 0, data: { articles: [] } }
      
      // 使用正则进行数据库模糊查询
      return await db.collection('cidi_posts').where(_.or([
        {
          title: db.RegExp({ regexp: keyword, options: 'i' }) // 匹配标题
        },
        {
          content: db.RegExp({ regexp: keyword, options: 'i' }) // 匹配内容
        }
      ])).get().then(res => {
        return { 
          code: 0, 
          data: { 
            articles: res.data, 
            locations: [] // 如果你也把地点存库了，这里也可以查地点表
          } 
        }
      })

    default:
      return { code: 404, msg: 'API Action Not Found' }
  }
}