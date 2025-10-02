package be.delomid.oneapp.mschat.mschat.repository;

import be.delomid.oneapp.mschat.mschat.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {

    List<Folder> findByApartmentIdAndParentFolderIsNull(Long apartmentId);

    List<Folder> findByParentFolderId(Long parentFolderId);

    @Query("SELECT f FROM Folder f WHERE f.apartment.id = :apartmentId AND f.parentFolder.id = :parentId")
    List<Folder> findByApartmentIdAndParentFolderId(@Param("apartmentId") Long apartmentId,
                                                     @Param("parentId") Long parentId);

    Optional<Folder> findByIdAndApartmentId(Long id, Long apartmentId);

    boolean existsByNameAndParentFolderIdAndApartmentId(String name, Long parentFolderId, Long apartmentId);

    boolean existsByNameAndParentFolderIsNullAndApartmentId(String name, Long apartmentId);
}
