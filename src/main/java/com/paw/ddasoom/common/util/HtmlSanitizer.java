package com.paw.ddasoom.common.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * 에디터 본문(HTML) 서버 측 sanitize.
 *
 * 프론트 DOMPurify(렌더 시점 방어)와 별개로, 저장 시점에 서버가 한 번 더 정제한다.
 * 에디터를 우회한 API 직접 호출(예: POST /api/posts 에 &lt;script&gt; 삽입)로
 * 악성 HTML이 DB에 저장되는 것(stored XSS)을 원천 차단하기 위함이다.
 * (SECURITY-FLOW의 "클라이언트 입력을 신뢰하지 않는다" 원칙과 동일한 철학)
 *
 * 에디터(RichTextEditor)는 POST / NOTICE / FAQ 공용이므로 sanitize 규칙도 단일 공용으로 둔다.
 * 도메인별로 규칙이 갈리면 허용 태그가 어긋나 위험/버그가 생기므로 common/util에 배치한다.
 */
public final class HtmlSanitizer {

    // 에디터(Tiptap StarterKit + Heading + Image + Link)가 생성하는 태그만 허용하는 화이트리스트.
    // 여기 없는 태그/속성(script, iframe, style, on* 이벤트 등)은 전부 제거된다.
    private static final Safelist EDITOR_SAFELIST = new Safelist()
            .addTags(
                    "p", "br", "hr",
                    "strong", "em", "s", "u", "code", "pre",
                    "h1", "h2", "h3", "h4", "h5", "h6",
                    "ul", "ol", "li", "blockquote",
                    "a", "img"
            )
            .addAttributes("a", "href")
            .addAttributes("img", "src", "alt", "data-image-id")   // data-image-id 필수 보존 (이미지 추적 설계)
            .addProtocols("a", "href", "http", "https", "mailto")  // javascript: 스킴 차단
            .addProtocols("img", "src", "http", "https");          // data:/blob: URI 차단

    // Jsoup 기본 출력의 pretty-print(개행·들여쓰기 삽입)를 꺼서 본문이 임의로 변형되지 않게 한다.
    private static final Document.OutputSettings NO_PRETTY_PRINT =
            new Document.OutputSettings().prettyPrint(false);

    private HtmlSanitizer() {
    }

    /**
     * 허용 태그만 남기고 나머지를 제거한 안전한 HTML을 반환한다.
     * baseUri는 빈 문자열 — 본문의 img/a는 절대 URL만 사용하므로 상대경로 해석이 불필요하다.
     */
    public static String sanitize(String html) {
        if (html == null) {
            return null;
        }
        return Jsoup.clean(html, "", EDITOR_SAFELIST, NO_PRETTY_PRINT);
    }
}