package org.daiso.cidi.modules.feedbackimage.repository;

import org.daiso.cidi.modules.feedbackimage.entity.FeedbackImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedbackImageRepository extends JpaRepository<FeedbackImage, Long> {
}
