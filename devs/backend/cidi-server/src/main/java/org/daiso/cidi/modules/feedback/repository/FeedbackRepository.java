/**
 * FeedbackRepository
 *
 * 说明：本文件定义了 FeedbackRepository，是 cidi-server 项目中的核心代码文件之一。
 *
 */

package org.daiso.cidi.modules.feedback.repository;

import org.daiso.cidi.modules.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}
