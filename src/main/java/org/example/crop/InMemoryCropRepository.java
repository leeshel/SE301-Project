package org.example.crop;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryCropRepository implements CropRepository {
    private final Map<String, CropProfile> profiles = new ConcurrentHashMap<>();

    @Override
    public void save(CropProfile profile) {
        profiles.put(profile.getName(), profile);
    }

    @Override
    public boolean remove(String name) {
        return profiles.remove(name) != null;
    }

    @Override
    public Optional<CropProfile> findByName(String name) {
        return Optional.ofNullable(profiles.get(name));
    }

    @Override
    public List<CropProfile> findAll() {
        return List.copyOf(profiles.values());
    }
}
