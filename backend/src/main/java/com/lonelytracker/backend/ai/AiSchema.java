package com.lonelytracker.backend.ai;

import java.util.Map;

/**
 * 구조화 출력에 넘길 스키마. 무엇을 받을지가 여기서 정해진다.
 *
 * <p>뿌리가 둘인 까닭은 규약마다 "없을 수 있는 값"을 적는 법이 달라서다.
 * OpenAI 호환은 타입에 null 을 더한 유니온을 받고, Claude 네이티브는 유니온을 받지 않아
 * 그 칸을 required 에서 빼는 방식만 쓴다. 어느 쪽을 쓸지는 규약이 고른다.
 */
interface AiSchema {

    /** 구조화 출력 스키마의 이름. 제공자가 응답을 가리킬 때 쓴다 */
    String name();

    /** null 유니온을 쓸 수 있는 규약용 뿌리 */
    Map<String, Object> strictRoot();

    /** 유니온을 못 쓰는 규약용 뿌리. 없는 값은 칸 자체가 빠져 온다 */
    Map<String, Object> optionalFieldsRoot();
}
