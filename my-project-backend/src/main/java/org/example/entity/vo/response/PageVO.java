package org.example.entity.vo.response;

import java.util.List;

public record PageVO<T>(long page, long size, long total, List<T> items) {
}
