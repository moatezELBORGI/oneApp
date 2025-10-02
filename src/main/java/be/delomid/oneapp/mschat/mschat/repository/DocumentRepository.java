package be.delomid.oneapp.mschat.mschat.repository;

import be.delomid.oneapp.mschat.mschat.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByFolderId(Long folderId);

    List<Document> findByApartmentId(Long apartmentId);

    Page<Document> findByApartmentId(Long apartmentId, Pageable pageable);

    Optional<Document> findByIdAndApartmentId(Long id, Long apartmentId);

    @Query("SELECT d FROM Document d WHERE d.apartment.id = :apartmentId " +
           "AND (LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(d.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Document> searchDocuments(@Param("apartmentId") Long apartmentId,
                                   @Param("search") String search);

    List<Document> findByFolderIdOrderByCreatedAtDesc(Long folderId);

    boolean existsByOriginalFilenameAndFolderId(String originalFilename, Long folderId);
}
