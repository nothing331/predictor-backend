package core.repository.adapter.db;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import db.entity.UserEntity;

@Repository
public interface JpaUserRepository extends JpaRepository<UserEntity, String> {
}
