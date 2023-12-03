package hcmute.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import hcmute.entity.AccountEntity;


@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Integer> {

}
