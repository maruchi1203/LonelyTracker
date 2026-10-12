package com.lonelytracker.backend.common;

/**
 * 사용자가 세운 차례를 가진 것.
 * <p>
 * 재정렬은 어느 테이블이든 같은 일이다 — 받은 id 차례대로 0부터 번호를 매긴다.
 * 그 일을 한 군데에 두려고 필요한 두 가지만 계약으로 꺼낸다.
 */
public interface Orderable {

    Long getId();

    void changeDisplayOrder(int displayOrder);
}
