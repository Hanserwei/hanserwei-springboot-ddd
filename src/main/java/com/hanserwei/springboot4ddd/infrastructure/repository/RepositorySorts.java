package com.hanserwei.springboot4ddd.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hanserwei.springboot4ddd.domain.page.SortOrder;

import java.util.List;
import java.util.Map;

/** 将外部排序属性映射为白名单列，并为分页补充唯一排序键。 */
public final class RepositorySorts {

    private RepositorySorts() {
    }

    public static void apply(QueryWrapper<?> wrapper, List<SortOrder> sorts,
                             Map<String, String> columns, String defaultColumn) {
        boolean applied = false;
        boolean sortedById = false;
        for (SortOrder sort : sorts) {
            String column = columns.get(sort.getProperty());
            if (column == null) {
                continue;
            }
            wrapper.orderBy(true, sort.getDirection() == SortOrder.Direction.ASC, column);
            applied = true;
            sortedById |= "id".equals(column);
        }
        if (!applied) {
            wrapper.orderByDesc(defaultColumn);
        }
        if (!sortedById) {
            wrapper.orderByDesc("id");
        }
    }
}
