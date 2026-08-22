package com.shipwise.config;

import java.util.List;

import javax.sql.DataSource;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.mapper.reflect.CaseInsensitiveColumnNameMatcher;
import org.jdbi.v3.core.mapper.reflect.SnakeCaseColumnNameMatcher;
import org.jdbi.v3.core.mapper.reflect.ReflectionMappers;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import com.shipwise.customer.Customer;
import com.shipwise.order.Order;
import com.shipwise.order.OrderItem;
import com.shipwise.product.Category;
import com.shipwise.product.Product;

@Configuration
public class JdbiConfig {

    @Bean
    public Jdbi jdbi(DataSource dataSource) {
        Jdbi jdbi = Jdbi.create(new TransactionAwareDataSourceProxy(dataSource));
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.getConfig(ReflectionMappers.class).setColumnNameMatchers(List.of(
                new SnakeCaseColumnNameMatcher(),
                new CaseInsensitiveColumnNameMatcher()
        ));
        jdbi.registerRowMapper(ConstructorMapper.factory(Category.class));
        jdbi.registerRowMapper(ConstructorMapper.factory(Product.class));
        jdbi.registerRowMapper(ConstructorMapper.factory(Customer.class));
        jdbi.registerRowMapper(ConstructorMapper.factory(Order.class));
        jdbi.registerRowMapper(ConstructorMapper.factory(OrderItem.class));
        return jdbi;
    }
}
