package com.lonelytracker.backend.common;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 받은 차례대로 0부터 번호를 다시 매긴다.
 * <p>
 * 사이 값을 쓰지 않는 까닭은 값이 촘촘해지면 결국 다시 매겨야 해서다. 애초에 매번 다
 * 매기면 그 일이 없다. 한 무리가 수십을 넘지 않아 비용이 문제되지 않는다.
 */
public final class ReorderUtil {

    private ReorderUtil() {
    }

    /**
     * 무리를 받은 차례로 세우고 번호를 매긴다.
     * <p>
     * ids 는 그 무리의 전부여야 한다. 일부만 받으면 나머지가 어디에 설지 정할 수 없고,
     * 남은 것을 뒤에 몰아 두면 사용자가 보던 자리와 달라진다. 밖에 있던 것을 데려오지도
     * 않는다 — 무리를 옮기는 일은 그 항목의 수정이 맡는다.
     *
     * @param group 지금 그 무리에 선 것 전부
     * @param ids   세울 차례
     * @param noun  오류 문구에 쓸 이름
     * @return 번호가 매겨진 것들. 부르는 쪽이 저장한다
     * @throws IllegalArgumentException 같은 id 가 둘이거나, 무리와 구성원이 어긋날 때
     */
    public static <T extends Orderable> List<T> arrange(List<T> group, List<Long> ids,
            String noun) {
        Set<Long> wanted = new HashSet<>(ids);
        if (wanted.size() != ids.size()) {
            throw new IllegalArgumentException("같은 " + noun + "를 두 번 보낼 수 없습니다");
        }

        Map<Long, T> byId = group.stream()
                .collect(Collectors.toMap(Orderable::getId, Function.identity()));

        if (!wanted.equals(byId.keySet())) {
            throw new IllegalArgumentException(
                    "그 무리의 " + noun + " 전부를 한 번에 보내 주세요");
        }

        List<T> arranged = ids.stream().map(byId::get).toList();
        for (int i = 0; i < arranged.size(); i++) {
            arranged.get(i).changeDisplayOrder(i);
        }
        return arranged;
    }
}
