
import com.fiap.cutwatch.domain.repository.KnifeRepository
import com.fiap.cutwatch.domain.repository.LoginRepository
import com.fiap.cutwatch.domain.repository.LoginRepositoryImpl
import com.fiap.cutwatch.domain.usecase.LoginUseCase
import com.fiap.cutwatch.view.form.FormViewModel
import com.fiap.cutwatch.view.login.LoginViewModel
import com.fiap.cutwatch.view.viewmodel.EstadoAppViewModel
import com.fiap.cutwatch.view.viewmodel.ListaKniveViewModel
import com.fiap.cutwatch.view.viewmodel.FormularioKnifeViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { FormularioKnifeViewModel(get()) }
    viewModel { ListaKniveViewModel(get()) }
    viewModel { EstadoAppViewModel() }
    viewModel { LoginViewModel(get()) }
    viewModel { FormViewModel(get()) }
}

val repositoryModule = module {
    single { KnifeRepository(get(), get()) }
    single <LoginRepository> { LoginRepositoryImpl(Firebase.auth) }
}

val firebaseModule = module {
    single { Firebase.firestore }
    single { Firebase.storage}
}

val useCaseModule = module {
    single { LoginUseCase(get()) }
}

val appModules: List<Module> = listOf(
    viewModelModule,
    repositoryModule,
    firebaseModule,
    useCaseModule
)