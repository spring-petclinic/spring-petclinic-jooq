package org.springframework.samples.petclinic.system;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public record Page<T>(List<T> content, Pageable pageable, long total) implements Iterable<T> {

	public Page(List<T> content, Pageable pageable, long total) {
		this.content = content;
		this.pageable = pageable;

		this.total = Optional.of(pageable)
			.filter(it -> !content.isEmpty())//
			.filter(it -> it.getOffset() + it.pageSize() > total)//
			.map(it -> it.getOffset() + content.size())//
			.orElse(total);
	}

	public Page(List<T> content) {
		this(content, Pageable.of(0, content.size()), content.size());
	}

	public long getTotalElements() {
		return total;
	}

	public List<T> getContent() {
		return content;
	}

	public boolean isEmpty() {
		return content.isEmpty();
	}

	public Iterator<T> iterator() {
		return content.iterator();
	}

	public int getTotalPages() {
		return getSize() == 0 ? 1 : (int) Math.ceil((double) total / (double) getSize());
	}

	public int getSize() {
		return pageable.pageSize();
	}

}
