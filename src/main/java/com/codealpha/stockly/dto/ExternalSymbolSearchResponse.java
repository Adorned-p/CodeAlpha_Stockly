package com.codealpha.stockly.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ExternalSymbolSearchResponse {

    private List<SymbolSearchResult> data;

    public ExternalSymbolSearchResponse() {
    }

    public List<SymbolSearchResult> getData() {
        return data;
    }

    public static class SymbolSearchResult {

        private String symbol;
        private String instrument_name;
        private String exchange;
        private String mic_code;
        private String exchange_timezone;
        private String instrument_type;
        private String country;
        private String currency;

        public SymbolSearchResult() {
        }

        public String getSymbol() {
            return symbol;
        }

        public String getInstrument_name() {
            return instrument_name;
        }

        public String getExchange() {
            return exchange;
        }

        public String getMic_code() {
            return mic_code;
        }

        public String getExchange_timezone() {
            return exchange_timezone;
        }

        public String getInstrument_type() {
            return instrument_type;
        }

        public String getCountry() {
            return country;
        }

        public String getCurrency() {
            return currency;
        }
    }
}