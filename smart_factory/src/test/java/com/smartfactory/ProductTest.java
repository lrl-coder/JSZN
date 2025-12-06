package com.smartfactory;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Product类的单元测试
 */
public class ProductTest {

    @Test
    public void testConstructor() {
        Product product = new Product(1, 4.0);
        assertNotNull(product);
    }

    @Test
    public void testGetters() {
        Product product = new Product(2, 3.5);
        
        assertEquals(2, product.getId());
        assertEquals(3.5, product.getUnitProcessingTime(), 0.001);
    }

    @Test
    public void testDifferentProcessingTimes() {
        // 测试不同的加工时间
        Product product1 = new Product(1, 2.0);
        Product product2 = new Product(2, 4.0);
        Product product3 = new Product(3, 6.0);
        
        assertEquals(2.0, product1.getUnitProcessingTime(), 0.001);
        assertEquals(4.0, product2.getUnitProcessingTime(), 0.001);
        assertEquals(6.0, product3.getUnitProcessingTime(), 0.001);
    }

    @Test
    public void testProductIds() {
        // 测试产品ID（应该是1, 2, 3）
        Product product1 = new Product(1, 4.0);
        Product product2 = new Product(2, 4.0);
        Product product3 = new Product(3, 4.0);
        
        assertEquals(1, product1.getId());
        assertEquals(2, product2.getId());
        assertEquals(3, product3.getId());
    }
}

