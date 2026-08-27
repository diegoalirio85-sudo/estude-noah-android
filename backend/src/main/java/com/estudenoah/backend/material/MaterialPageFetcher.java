package com.estudenoah.backend.material;

import java.net.URI;

public interface MaterialPageFetcher {
    FetchedMaterialPage fetch(URI uri);
}
