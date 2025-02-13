package flyway.ubigushared;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.oskari.helpers.AppSetupHelper;
import org.oskari.helpers.LayerHelper;

public class V1_0_3__initial_db_content extends BaseJavaMigration {

    public void migrate(Context context) throws Exception {
        // add applications based on json under /src/main/resources/json/apps/
        AppSetupHelper.create(context.getConnection(), "/json/apps/ubigu-3067.json");

        AppSetupHelper.create(context.getConnection(), "/json/apps/publisher-template.json");

        LayerHelper.setupLayer("/json/layers/mml-maastokartta.json");
        LayerHelper.setupLayer("/json/layers/mml-selkokartta.json");
        LayerHelper.setupLayer("/json/layers/mml-ortokuva.json");

    }
}
