package be.delomid.oneapp.mschat.mschat.repository;

 import be.delomid.oneapp.mschat.mschat.model.Message;
 import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE m.channel.id = :channelId AND m.isDeleted = false ORDER BY m.createdAt DESC")
    Page<Message> findByChannelIdOrderByCreatedAtDesc(@Param("channelId") Long channelId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.channel.id = :channelId AND m.senderId = :senderId AND m.isDeleted = false ORDER BY m.createdAt DESC")
    Page<Message> findByChannelIdAndSenderIdOrderByCreatedAtDesc(
        @Param("channelId") Long channelId, 
        @Param("senderId") String senderId, 
        Pageable pageable
    );

    @Query("SELECT COUNT(m) FROM Message m WHERE m.channel.id = :channelId AND m.isDeleted = false")
    Long countByChannelId(@Param("channelId") Long channelId);
}