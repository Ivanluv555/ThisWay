/**
 * FeedbackImageRepository
 *
 * 说明：本文件定义了 FeedbackImageRepository，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.feedbackimage.repository;

import org.daiso.cidi.modules.feedbackimage.entity.FeedbackImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackImageRepository extends JpaRepository<FeedbackImage, Long> {
}
