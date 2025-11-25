package co.com.extractor.appservice.adapter;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.InputStream;

@Component
public class ParseRutUseCaseAdapter {
    private final co.com.extractor.usecase.ParseRutUseCase useCase;

    @Autowired
    public ParseRutUseCaseAdapter(co.com.extractor.usecase.ParseRutUseCase useCase) {
        this.useCase = useCase;
    }

    public co.com.extractor.domain.model.RutResponse execute(InputStream is, String filename) throws Exception {
        return useCase.execute(is, filename);
    }
}
