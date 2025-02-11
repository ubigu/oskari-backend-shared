package flyway.ubigushared;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.oskari.helpers.AppSetupHelper;

public class V1_0_3__initial_db_content extends BaseJavaMigration {

    public void migrate(Context context) throws Exception {
        // add applications based on json under /src/main/resources/json/apps/
        AppSetupHelper.create(context.getConnection(), "/json/apps/ubigu-3067.json");
    }
}
