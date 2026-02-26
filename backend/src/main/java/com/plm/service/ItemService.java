package com.plm.service;

import com.plm.dto.ItemRequest;
import com.plm.dto.ItemResponse;
import com.plm.entity.Item;
import com.plm.entity.LifecycleState;
import com.plm.exception.BadRequestException;
import com.plm.exception.ConflictException;
import com.plm.exception.ResourceNotFoundException;
import com.plm.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public List<ItemResponse> getAllItems() {
        return itemRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ItemResponse getItemById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional(readOnly = true)
    public ItemResponse getItemByNumber(String itemNumber) {
        return toResponse(itemRepository.findByItemNumber(itemNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + itemNumber)));
    }

    @Transactional
    public ItemResponse createItem(ItemRequest request) {
        if (itemRepository.existsByItemNumber(request.getItemNumber())) {
            throw new ConflictException("Item number already exists: " + request.getItemNumber());
        }
        Item item = Item.builder()
                .itemNumber(request.getItemNumber())
                .name(request.getName())
                .description(request.getDescription())
                .lifecycleState(request.getLifecycleState() != null ? request.getLifecycleState() : LifecycleState.DRAFT)
                .build();
        return toResponse(itemRepository.save(item));
    }

    @Transactional
    public ItemResponse updateItem(Long id, ItemRequest request) {
        Item item = findById(id);
        if (!item.getItemNumber().equals(request.getItemNumber())
                && itemRepository.existsByItemNumber(request.getItemNumber())) {
            throw new ConflictException("Item number already exists: " + request.getItemNumber());
        }
        item.setItemNumber(request.getItemNumber());
        item.setName(request.getName());
        item.setDescription(request.getDescription());
        return toResponse(itemRepository.save(item));
    }

    @Transactional
    public ItemResponse transitionLifecycle(Long id, LifecycleState newState) {
        Item item = findById(id);
        validateTransition(item.getLifecycleState(), newState);
        item.setLifecycleState(newState);
        return toResponse(itemRepository.save(item));
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> search(String q) {
        return itemRepository.search(q).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void deleteItem(Long id) {
        Item item = findById(id);
        if (item.getLifecycleState() == LifecycleState.RELEASED) {
            throw new BadRequestException("Cannot delete a RELEASED item");
        }
        itemRepository.delete(item);
    }

    private void validateTransition(LifecycleState current, LifecycleState next) {
        boolean valid = switch (current) {
            case DRAFT -> next == LifecycleState.IN_REVIEW;
            case IN_REVIEW -> next == LifecycleState.RELEASED || next == LifecycleState.DRAFT;
            case RELEASED -> next == LifecycleState.OBSOLETE;
            case OBSOLETE -> false;
        };
        if (!valid) {
            throw new BadRequestException(
                    String.format("Invalid lifecycle transition: %s -> %s", current, next));
        }
    }

    private Item findById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + id));
    }

    private ItemResponse toResponse(Item item) {
        ItemResponse r = new ItemResponse();
        r.setId(item.getId());
        r.setItemNumber(item.getItemNumber());
        r.setName(item.getName());
        r.setDescription(item.getDescription());
        r.setLifecycleState(item.getLifecycleState());
        r.setCreatedAt(item.getCreatedAt());
        r.setUpdatedAt(item.getUpdatedAt());
        return r;
    }
}
