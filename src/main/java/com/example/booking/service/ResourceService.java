package com.example.booking.service;

import com.example.booking.dto.*;
import com.example.booking.entity.Resource;
import com.example.booking.repository.ResourceRepository;
import com.example.booking.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ResourceService {

    private final ResourceRepository repository;

    public ResourceService(ResourceRepository repository) {
        this.repository = repository;
    }

    public List<ResourceResponse> getResources(boolean activeOnly) {
        return (activeOnly ? repository.findByActiveTrue() : repository.findAll())
                .stream().map(ResourceResponse::from).toList();
    }

    public ResourceResponse getById(Long id) {
        return ResourceResponse.from(find(id));
    }

    public ResourceResponse create(ResourceRequest request) {
        Resource resource = new Resource(
                request.name(), request.type(), request.description(),
                request.pricePerHour(),
                request.active() == null || request.active()
        );
        return ResourceResponse.from(repository.save(resource));
    }

    public ResourceResponse update(Long id, ResourceRequest request) {
        Resource resource = find(id);
        resource.setName(request.name());
        resource.setType(request.type());
        resource.setDescription(request.description());
        resource.setPricePerHour(request.pricePerHour());
        if (request.active() != null) {
            resource.setActive(request.active());
        }
        return ResourceResponse.from(repository.save(resource));
    }

    public void delete(Long id) {
        repository.delete(find(id));
    }

    public Resource find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resource not found: " + id));
    }
}
