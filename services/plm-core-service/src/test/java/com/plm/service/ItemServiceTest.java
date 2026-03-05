package com.plm.service;

import com.plm.dto.ItemRequest;
import com.plm.dto.ItemResponse;
import com.plm.entity.Item;
import com.plm.entity.LifecycleState;
import com.plm.exception.BadRequestException;
import com.plm.exception.ConflictException;
import com.plm.exception.ResourceNotFoundException;
import com.plm.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    private Item item;

    @BeforeEach
    void setUp() {
        item = Item.builder()
                .id(1L)
                .itemNumber("ITM-001")
                .name("Test Item")
                .description("A test item")
                .lifecycleState(LifecycleState.DRAFT)
                .build();
    }

    @Test
    void createItem_success() {
        ItemRequest request = new ItemRequest();
        request.setItemNumber("ITM-001");
        request.setName("Test Item");
        request.setDescription("A test item");

        when(itemRepository.existsByItemNumber("ITM-001")).thenReturn(false);
        when(itemRepository.save(any())).thenReturn(item);

        ItemResponse response = itemService.createItem(request);

        assertThat(response.getItemNumber()).isEqualTo("ITM-001");
        assertThat(response.getLifecycleState()).isEqualTo(LifecycleState.DRAFT);
    }

    @Test
    void createItem_duplicateNumber_throwsConflict() {
        ItemRequest request = new ItemRequest();
        request.setItemNumber("ITM-001");
        request.setName("Test Item");

        when(itemRepository.existsByItemNumber("ITM-001")).thenReturn(true);

        assertThatThrownBy(() -> itemService.createItem(request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void getItemById_notFound_throwsNotFound() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> itemService.getItemById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void transitionLifecycle_validTransition_success() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(itemRepository.save(any())).thenReturn(item);

        itemService.transitionLifecycle(1L, LifecycleState.IN_REVIEW);

        verify(itemRepository).save(item);
        assertThat(item.getLifecycleState()).isEqualTo(LifecycleState.IN_REVIEW);
    }

    @Test
    void transitionLifecycle_invalidTransition_throwsBadRequest() {
        item.setLifecycleState(LifecycleState.OBSOLETE);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> itemService.transitionLifecycle(1L, LifecycleState.DRAFT))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void deleteItem_released_throwsBadRequest() {
        item.setLifecycleState(LifecycleState.RELEASED);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> itemService.deleteItem(1L))
                .isInstanceOf(BadRequestException.class);
    }
}
