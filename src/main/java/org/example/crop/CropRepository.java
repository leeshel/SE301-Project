package org.example.crop;

import java.util.List;
import java.util.Optional;

public interface CropRepository {
    void save(CropProfile profile);
    
    boolean remove(String name);

    Optional<CropProfile> findByName(String name);

    List<CropProfile> findAll();
}
