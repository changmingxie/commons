package org.mengyun.commons.collapser;

import java.util.List;
import java.util.Map;

public interface Callable<T, R> {

    Map<T,R> call(List<T> requests);
}
