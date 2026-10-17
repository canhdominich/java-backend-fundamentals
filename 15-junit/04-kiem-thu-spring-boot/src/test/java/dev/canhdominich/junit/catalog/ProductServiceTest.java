package dev.canhdominich.junit.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository products;

    @InjectMocks
    ProductService service;

    @Test
    void findsAnExistingProduct() {
        Product book = new Product("Book", 15_000);
        when(products.findById(1L)).thenReturn(Optional.of(book));

        assertThat(service.find(1L)).isSameAs(book);
    }

    @Test
    void reportsAMissingProduct() {
        when(products.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.find(7L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("product 7 not found");
    }

    @Test
    void savesWhatItCreates() {
        when(products.save(org.mockito.ArgumentMatchers.any(Product.class))).thenAnswer(call -> call.getArgument(0));

        Product created = service.create("Pen", 5_000);

        assertThat(created.getName()).isEqualTo("Pen");
        verify(products).save(created);
    }
}
