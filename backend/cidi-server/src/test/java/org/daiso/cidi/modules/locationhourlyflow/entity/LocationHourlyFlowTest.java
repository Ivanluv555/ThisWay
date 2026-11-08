package org.daiso.cidi.modules.locationhourlyflow.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class LocationHourlyFlowTest {
    /**
     * 首次持久化应自动填写创建时间。
     * 输入没有创建时间的实体，预期回调将时间设为调用期间的当前时间。
     */
    @Test
    void onPersistInitializesMissingCreationTime() {
        LocationHourlyFlow entity = new LocationHourlyFlow();
        LocalDateTime before = LocalDateTime.now();
        entity.onPersist();
        assertThat(entity.getCreatedAt()).isBetween(before, LocalDateTime.now());
    }

    /**
     * 历史导入记录应保留显式创建时间，重复持久化回调也不能覆盖它。
     * 输入已有固定创建时间的实体，预期两次回调后仍为原值。
     */
    @Test
    void onPersistPreservesExistingCreationTime() {
        LocalDateTime original = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocationHourlyFlow entity = LocationHourlyFlow.builder().createdAt(original).build();
        entity.onPersist();
        entity.onPersist();
        assertThat(entity.getCreatedAt()).isEqualTo(original);
    }

    /**
     * 两种创建方式应共享模拟数据来源默认值，但显式来源不得被覆盖。
     * 输入无参构造、缺省构建器及显式来源构建器，预期前两者为模拟来源，后者保留传入值。
     */
    @Test
    void sourceDefaultsApplyWithoutOverridingExplicitValues() {
        assertThat(new LocationHourlyFlow().getDataSource()).isEqualTo("simulated");
        assertThat(LocationHourlyFlow.builder().build().getDataSource()).isEqualTo("simulated");
        assertThat(LocationHourlyFlow.builder().dataSource("sensor").build().getDataSource()).isEqualTo("sensor");
        assertThat(LocationHourlyFlow.builder().dataSource(null).build().getDataSource()).isNull();
    }
}
