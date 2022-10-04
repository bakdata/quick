/*
 *    Copyright 2022 bakdata GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.bakdata.quick.gateway.fetcher;

import java.util.List;
import lombok.Builder;
import lombok.Value;

public class TestModels {
    @Value
    @Builder
    public static class PurchaseList {
        String purchaseId;
        List<Integer> productIds;
    }

    @Value
    @Builder
    public static class Purchase {
        String purchaseId;
        int productId;
        int amount;
        Price price;
    }

    @Value
    @Builder
    public static class Price {
        String currencyId;
        double value;
    }

    @Value
    @Builder
    public static class Product {
        int productId;
        String name;
        List<Integer> prices;
        int ratings;
        Price price;
    }

    @Value
    @Builder
    public static class Currency {
        String currencyId;
        String currency;
        double rate;
    }
}
