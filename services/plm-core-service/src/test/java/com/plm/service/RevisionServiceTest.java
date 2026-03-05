package com.plm.service;

import com.plm.dto.RevisionResponse;
import com.plm.entity.Item;
import com.plm.entity.LifecycleState;
import com.plm.entity.Revision;
import com.plm.entity.RevisionStatus;
import com.plm.exception.ResourceNotFoundException;
import com.plm.repository.ItemRepository;
import com.plm.repository.RevisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RevisionServiceTest {

    @Mock
    private RevisionRepository revisionRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private RevisionService revisionService;

    private Item item;
    private Revision revisionA;

    @BeforeEach
    void setUp() {
        item = Item.builder().id(1L).itemNumber("ITM-001").name("Test").lifecycleState(LifecycleState.DRAFT).build();
        revisionA = Revision.builder().id(1L).item(item).revisionCode("A").status(RevisionStatus.IN_WORK).build();
    }

    @Test
    void nextRevision_emptyList_createsRevisionA() {
        when(itemRepository.existsById(1L)).thenReturn(true);
        when(revisionRepository.findByItemIdOrderByRevisionCodeAsc(1L)).thenReturn(List.of());
        when(itemRepository.getReferenceById(1L)).thenReturn(item);
        when(revisionRepository.save(any())).thenReturn(revisionA);

        RevisionResponse response = revisionService.nextRevision(1L);

        assertThat(response.getRevisionCode()).isEqualTo("A");
    }

    @Test
    void nextRevision_existingA_createsRevisionB() {
        Revision revisionB = Revision.builder().id(2L).item(item).revisionCode("B").status(RevisionStatus.IN_WORK).build();
        when(itemRepository.existsById(1L)).thenReturn(true);
        when(revisionRepository.findByItemIdOrderByRevisionCodeAsc(1L)).thenReturn(List.of(revisionA));
        when(itemRepository.getReferenceById(1L)).thenReturn(item);
        when(revisionRepository.save(any())).thenReturn(revisionB);

        RevisionResponse response = revisionService.nextRevision(1L);

        assertThat(response.getRevisionCode()).isEqualTo("B");
    }

    @Test
    void getRevisionsByItem_itemNotFound_throwsNotFound() {
        when(itemRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> revisionService.getRevisionsByItem(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
