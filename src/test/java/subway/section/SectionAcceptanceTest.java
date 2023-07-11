package subway.section;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import subway.AcceptanceTest;
import subway.line.LineStep;
import subway.station.StationStep;

/**
 * 프로그래밍 요구사항
 * - 인수 테스트 주도 개발 프로세스에 맞춰서 기능을 구현하세요.
 *   - 요구사항 설명을 참고하여 인수 조건을 정의
 *   - 인수 조건을 검증하는 인수 테스트 작성
 *   - 인수 테스트를 충족하는 기능 구현
 * - 인수 조건은 인수 테스트 메서드 상단에 주석으로 작성하세요.
 *   - 뼈대 코드의 인수 테스트를 참고
 * - 인수 테스트의 결과가 다른 인수 테스트에 영향을 끼치지 않도록 인수 테스트를 서로 격리 시키세요.
 * - 인수 테스트의 재사용성과 가독성, 그리고 빠른 테스트 의도 파악을 위해 인수 테스트를 리팩터링 하세요.
 */
@DisplayName("지하철 구간 관련 기능")
public class SectionAcceptanceTest extends AcceptanceTest {
    /*
      # 구간 등록 기능
      ## 요구사항
      - 지하철 노선에 구간을 등록한다.
      - 새로운 구간의 상행역은 해당 노선에 등록되어있는 하행 종점역이어야 한다. 즉, 새로운 구간이 등록될 때, "기존 구간의 하행역 == 새로운 구간의 상행 역"이여야 등록 가능하다.
      - 새로운 구간의 하행역은 해당 노선에 등록되어있는 역일 수 없다. -> 하행역이 N개가 될 수 있으므로..
      - 새로운 구간 등록시 위 조건에 부합하지 않는 경우 에러 처리한다.
     */

    /**
     * Given : 지하철역을 3개 생성하고
     * And : 지하철 노선을 1개 생성한 후
     * When : 새로운 구간을 등록하면
     * Then : 노선에 새로운 구간이 등록된다
     */
    @DisplayName("지하철 구간 등록")
    @Test
    void registerSectionOk() {
        // given
        long 노선_상행_Id = 응답_결과에서_Id를_추출한다(StationStep.지하철역을_생성한다("강남역"));
        long 노선_하행_Id = 응답_결과에서_Id를_추출한다(StationStep.지하철역을_생성한다("양재역"));
        long 구간_하행_Id = 응답_결과에서_Id를_추출한다(StationStep.지하철역을_생성한다("양재시민의숲역"));

        long lineId = 응답_결과에서_Id를_추출한다(LineStep.지하철_노선을_생성한다(노선_상행_Id, 노선_하행_Id, "신분당선"));

        // when
        ExtractableResponse<Response> createSectionResponse = SectionStep.지하철_노선_구간을_등록한다(lineId, 노선_하행_Id, 구간_하행_Id);

        // then
        assertThat(createSectionResponse.statusCode()).isEqualTo(HttpStatus.OK.value());

        ExtractableResponse<Response> showLineResponse = LineStep.지하철_노선을_조회한다(lineId);
        assertThat(지하철_구간_목록을_추출한다(showLineResponse)).hasSize(2);
    }

    private long 응답_결과에서_Id를_추출한다(ExtractableResponse<Response> responseOfCreateStation) {
        return responseOfCreateStation.jsonPath().getLong("id");
    }

    private List<Object> 지하철_구간_목록을_추출한다(ExtractableResponse<Response> showLineResponse) {
        return showLineResponse.jsonPath().getList("sections");
    }

    /**
     * Given : 지하철역을 4개 생성하고
     * And : 새로운 노선을 1개 생성한 후
     * When : 상행역이 해당 노선의 하행 종점역이 아닌 새로운 구간을 등록하면
     * Then : 예외가 발생한다
     */
    @DisplayName("지하철 구간 등록 예외 케이스 : 노선의 하행 종점역 != 새로운 노선의 상행역")
    @Test
    void registerSectionFailCase1() {
        // given
        long 노선_상행_Id = 응답_결과에서_Id를_추출한다(StationStep.지하철역을_생성한다("강남역"));
        long 노선_하행_Id = 응답_결과에서_Id를_추출한다(StationStep.지하철역을_생성한다("양재역"));
        long 구간_상행_Id = 응답_결과에서_Id를_추출한다(StationStep.지하철역을_생성한다("여의도역"));
        long 구간_하행_Id = 응답_결과에서_Id를_추출한다(StationStep.지하철역을_생성한다("마곡나루역"));

        long lineId = 응답_결과에서_Id를_추출한다(LineStep.지하철_노선을_생성한다(노선_상행_Id, 노선_하행_Id, "신분당선"));

        // when
        ExtractableResponse<Response> createSectionResponse = SectionStep.지하철_노선_구간을_등록한다(lineId, 구간_상행_Id, 구간_하행_Id);

        // then
        assertThat(createSectionResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(응답_결과에서_Message를_추출한다(createSectionResponse)).contains("새로운 구간의 상행역은 노선에 등록된 마지막 구간의 하행역과 같아야 합니다.");
    }

    private String 응답_결과에서_Message를_추출한다(ExtractableResponse<Response> createSectionResponse) {
        return createSectionResponse.jsonPath().getString("message");
    }

    /**
     * Given : 지하철역을 4개 생성하고
     * And : 새로운 노선을 1개 생성한 후
     * When : 해당 노선에 등록되어 있는 하행 역을 가진 새로운 구간을 등록하면
     * Then : 예외가 발생한다
     */
    @DisplayName("지하철 구간 등록 예외 케이스 : 노선에 등록된 하행 역을 가진 새로운 구간 등록")
    @Test
    void registerSectionFailCase2() {
        // given
        ExtractableResponse<Response> 강남역 = StationStep.지하철역을_생성한다("강남역");

        long 노선_상행_Id = 응답_결과에서_Id를_추출한다(강남역);
        long 노선_하행_Id = 응답_결과에서_Id를_추출한다(StationStep.지하철역을_생성한다("양재역"));
        long 구간_하행_Id = 응답_결과에서_Id를_추출한다(강남역);

        long lineId = 응답_결과에서_Id를_추출한다(LineStep.지하철_노선을_생성한다(노선_상행_Id, 노선_하행_Id, "신분당선"));

        // when
        ExtractableResponse<Response> createSectionResponse = SectionStep.지하철_노선_구간을_등록한다(lineId, 노선_하행_Id, 구간_하행_Id);

        // then
        assertThat(createSectionResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }


    /*
      # 구간 제거 기능
      ## 요구사항
      - 지하철 노선에 구간을 제거한다.
      - 지하철 노선에 등록된 "하행 종점역"만 제거할 수 있다. (마지막 구간만 제거)
      - 지하철 노선에 상행 종점역과 하행 종점역만 있는 경우, 즉 구간이 1개인 경우 역을 삭제할 수 없다.
      - 새로운 구간 제거 시, 위 조건에 부합하지 않는 경우 에러 처리한다.
      ## Request
      - DELETE /lines/{lineId}/sections?stationId={stationId}
      ## 시나리오
     */

    /**
     * Given : 지하철역을 3개 생성하고
     * And : 지하철 노선을 1개 생성하고
     * And : 새로운 구간을 1개 등록한 후
     * When : 하행 종점역을 제거하면
     * Then : 구간이 삭제된다
     */
    @DisplayName("지하철 구간 삭제")
    @Test
    void deleteSectionOk() {
        // given
        long 노선_상행_Id = 응답_결과에서_Id를_추출한다(StationStep.지하철역을_생성한다("강남역"));
        long 노선_하행_Id = 응답_결과에서_Id를_추출한다(StationStep.지하철역을_생성한다("양재역"));
        long 구간_하행_Id = 응답_결과에서_Id를_추출한다(StationStep.지하철역을_생성한다("양재시민의숲역"));

        long lineId = 응답_결과에서_Id를_추출한다(LineStep.지하철_노선을_생성한다(노선_상행_Id, 노선_하행_Id, "신분당선"));

        SectionStep.지하철_노선_구간을_등록한다(lineId, 노선_하행_Id, 구간_하행_Id);

        // when
        ExtractableResponse<Response> deleteSectionResponse = SectionStep.지하철_구간을_삭제한다(lineId, 구간_하행_Id);

        // then
        assertThat(deleteSectionResponse.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());

        ExtractableResponse<Response> showLineResponse = LineStep.지하철_노선을_조회한다(lineId);
        assertThat(지하철_구간_목록을_추출한다(showLineResponse)).hasSize(1);
    }

    /**
     * Given : 지하철역을 3개 생성하고
     * And : 지하철 노선을 1개 생성하고
     * And : 새로운 구간을 1개 등록한 후
     * When : 하행 종점역(마지막 구간)이 아닌 구간을 제거하면
     * Then : 예외가 발생한다.
     */
    @DisplayName("지하철 구간 삭제 예외 케이스 : 하행 종점역이 아닌 구간을 제거 (마지막 구간의 상행 종점역을 입력)")
    @Test
    void deleteSectionFailCase1() {
        // given
        long 노선_상행_Id = 응답_결과에서_Id를_추출한다(StationStep.지하철역을_생성한다("강남역"));
        long 노선_하행_Id = 응답_결과에서_Id를_추출한다(StationStep.지하철역을_생성한다("양재역"));
        long 구간_하행_Id = 응답_결과에서_Id를_추출한다(StationStep.지하철역을_생성한다("양재시민의숲역"));

        long lineId = 응답_결과에서_Id를_추출한다(LineStep.지하철_노선을_생성한다(노선_상행_Id, 노선_하행_Id, "신분당선"));

        SectionStep.지하철_노선_구간을_등록한다(lineId, 노선_하행_Id, 구간_하행_Id);

        // when
        ExtractableResponse<Response> deleteSectionResponse = SectionStep.지하철_구간을_삭제한다(lineId, 노선_하행_Id);

        // then
        assertThat(deleteSectionResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("지하철 구간 삭제 예외 케이스 : 하행 종점역이 아닌 구간을 제거 (마지막 구간이 가지지 않은 역을 입력)")
    @Test
    void deleteSectionFailCase2() {
        // given
        long 노선_상행_Id = 응답_결과에서_Id를_추출한다(StationStep.지하철역을_생성한다("강남역"));
        long 노선_하행_Id = 응답_결과에서_Id를_추출한다(StationStep.지하철역을_생성한다("양재역"));
        long 구간_하행_Id = 응답_결과에서_Id를_추출한다(StationStep.지하철역을_생성한다("양재시민의숲역"));

        long lineId = 응답_결과에서_Id를_추출한다(LineStep.지하철_노선을_생성한다(노선_상행_Id, 노선_하행_Id, "신분당선"));

        SectionStep.지하철_노선_구간을_등록한다(lineId, 노선_하행_Id, 구간_하행_Id);

        // when
        ExtractableResponse<Response> deleteSectionResponse = SectionStep.지하철_구간을_삭제한다(lineId, 노선_상행_Id);

        // then
        assertThat(deleteSectionResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    /**
     * Given : 지하철역을 2개 생성하고
     * And : 지하철 노선을 1개 생성한 후
     * When : 하행 종점역을 제거하면
     * Then : 예외가 발생한다.
     */
    @DisplayName("지하철 구간 삭제 예외 케이스 : 구간이 1개일 때 하행 종점역을 제거")
    @Test
    void deleteSectionFailCase3() {
        // given
        long 노선_상행_Id = 응답_결과에서_Id를_추출한다(StationStep.지하철역을_생성한다("강남역"));
        long 노선_하행_Id = 응답_결과에서_Id를_추출한다(StationStep.지하철역을_생성한다("양재역"));

        long lineId = 응답_결과에서_Id를_추출한다(LineStep.지하철_노선을_생성한다(노선_상행_Id, 노선_하행_Id, "신분당선"));

        // when
        ExtractableResponse<Response> deleteSectionResponse = SectionStep.지하철_구간을_삭제한다(lineId, 노선_하행_Id);
        
        // then
        assertThat(deleteSectionResponse.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }
}
