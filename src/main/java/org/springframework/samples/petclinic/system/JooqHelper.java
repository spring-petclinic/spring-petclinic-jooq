package org.springframework.samples.petclinic.system;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Select;
import org.jooq.Table;

import static org.jooq.impl.DSL.*;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.max;

/**
 * @see <a href=
 * "https://blog.jooq.org/calculating-pagination-metadata-without-extra-roundtrips-in-sql/">
 * Calculating Pagination Metadata Without Extra Roundtrips in SQL</a>
 */
public class JooqHelper {

	private JooqHelper() {
		// Prevent instantiation
	}

	public static Select<?> paginate(DSLContext dsl, Select<?> original, Field<?>[] sort, long limit, long offset) {
		Table<?> u = original.asTable("u");
		Field<Integer> totalRows = count().over().as("total_rows");
		Field<Integer> row = rowNumber().over().orderBy(u.fields(sort)).as("row");

		Table<?> t = dsl.select(u.asterisk())
			.select(totalRows, row)
			.from(u)
			.orderBy(u.fields(sort))
			.limit(limit)
			.offset(offset)
			.asTable("t");

		return dsl.select(t.fields(original.getSelect().toArray(Field[]::new)))
			.select(count().over().as("actual_page_size"),
					field(max(t.field(row)).over().eq(t.field(totalRows))).as("last_page"), t.field(totalRows),
					t.field(row), t.field(row).minus(inline(1)).div(limit).plus(inline(1)).as("current_page"))
			.from(t)
			.orderBy(t.fields(sort));
	}

}
