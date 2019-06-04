package co.smartreceipts.android.di;

import co.smartreceipts.android.SmartReceiptsApplication;
import co.smartreceipts.android.di.scopes.ApplicationScope;
import dagger.Component;
import dagger.android.AndroidInjectionModule;

@ApplicationScope
@Component(modules = {
        AndroidInjectionModule.class,
        FlavorModule.class,
        GlobalBindingModule.class,
        BaseAppModule.class
})
public interface AppComponent {

    SmartReceiptsApplication inject(SmartReceiptsApplication application);
}
