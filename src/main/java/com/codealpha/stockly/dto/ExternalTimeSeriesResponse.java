package com.codealpha.stockly.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ExternalTimeSeriesResponse {

    private String status;
    private String message;

    private Meta meta;

    private List<TimeSeriesValue> values;

    public ExternalTimeSeriesResponse() {
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public Meta getMeta() {
        return meta;
    }

    public List<TimeSeriesValue> getValues() {
        return values;
    }

    public static class Meta {

        private String symbol;
        private String interval;

        public Meta() {
        }

        public String getSymbol() {
            return symbol;
        }

        public String getInterval() {
            return interval;
        }
    }

    public static class TimeSeriesValue {

        private String datetime;

        private String open;

        private String high;

        private String low;

        private String close;

        private String volume;

        public TimeSeriesValue() {
        }

        public String getDatetime() {
            return datetime;
        }

        public String getOpen() {
            return open;
        }

        public String getHigh() {
            return high;
        }

        public String getLow() {
            return low;
        }

        public String getClose() {
            return close;
        }

        public String getVolume() {
            return volume;
        }
    }
}