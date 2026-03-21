package org.daiso.cidi.modules.postimage.repository;

import org.daiso.cidi.modules.postimage.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {
}
