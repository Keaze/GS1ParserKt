import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveAtLeastSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.app.gs1parser.GS1Scanner
import org.app.gs1parser.GS1_AI_FILE
import org.app.gs1parser.data.Gs1Ai
import org.app.utils.JsonResourceReader

class JsonResourceReaderTest : ShouldSpec({
    should("find the resource file") {
        JsonResourceReader.resourceExists(GS1_AI_FILE) shouldBe true
    }

    shouldNotBeNull {
        JsonResourceReader.readJsonFromResource<List<Gs1Ai>>(GS1_AI_FILE)
    }
    should("have 516 elements") {
        JsonResourceReader.readJsonFromResource<List<Gs1Ai>>(GS1_AI_FILE)?.shouldHaveAtLeastSize(500)
    }
}
)