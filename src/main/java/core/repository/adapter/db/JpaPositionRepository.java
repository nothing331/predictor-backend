package core.repository.adapter.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import db.entity.PositionEntity;
import java.util.List;

@Repository
public interface JpaPositionRepository extends JpaRepository<PositionEntity, Long> {
    List<PositionEntity> findByUserId(String userId);

    void deleteByUserId(String userId); // Caution: deletes all positions for reload? Or standard update?
}
