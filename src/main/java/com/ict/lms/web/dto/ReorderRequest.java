package com.ict.lms.web.dto;

import java.util.List;

/** The full list of class ids, in the teacher's new drag-to-reorder sequence. */
public record ReorderRequest(List<Long> classIds) {
}
