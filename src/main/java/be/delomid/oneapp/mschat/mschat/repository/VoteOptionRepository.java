package be.delomid.oneapp.mschat.mschat.repository;

import be.delomid.oneapp.mschat.mschat.model.VoteOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteOptionRepository extends JpaRepository<VoteOption, Long> {
    
    @Query("SELECT COUNT(uv) FROM UserVote uv WHERE uv.voteOption.id = :optionId")
    Long countVotesByOptionId(@Param("optionId") Long optionId);
}