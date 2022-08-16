/**
 * Copyright 2018 The Feign Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package reactivefeign.webclient;

import java.time.Duration;
import reactivefeign.ReactiveOptions;

/**
 * @author Sergii Karpenko
 */
public class WebReactiveOptions extends ReactiveOptions {

  public static final WebReactiveOptions DEFAULT_OPTIONS = (WebReactiveOptions)new WebReactiveOptions.Builder()
          .setReadTimeoutMillis(10000)
          .setWriteTimeoutMillis(10000)
          .setConnectTimeoutMillis(5000)
          .build();

  private final Long readTimeoutMillis;
  private final Long writeTimeoutMillis;
  private boolean keepAlive;
  private Duration maxIdleTime;
  private Duration maxLifeTime;
  private Integer maxConnections = 500;
  private Duration pendingAcquireTimeout;
  private Duration evictInBackground;

  private WebReactiveOptions(Boolean useHttp2, Long connectTimeoutMillis, Boolean tryUseCompression,
      Long readTimeoutMillis, Long writeTimeoutMillis, boolean keepAlive, Duration maxIdleTime,
      Duration maxLifeTime, Integer maxConnections, Duration pendingAcquireTimeout, Duration evictInBackground) {
    super(useHttp2, connectTimeoutMillis, tryUseCompression);
    this.readTimeoutMillis = readTimeoutMillis;
    this.writeTimeoutMillis = writeTimeoutMillis;
    this.keepAlive = keepAlive;
    this.maxIdleTime = maxIdleTime;
    this.maxLifeTime = maxLifeTime;
    this.maxConnections = maxConnections;
    this.pendingAcquireTimeout = pendingAcquireTimeout;
    this.evictInBackground = evictInBackground;
  }

  public Long getReadTimeoutMillis() {
    return readTimeoutMillis;
  }

  public Long getWriteTimeoutMillis() {
    return writeTimeoutMillis;
  }

  public boolean isKeepAlive() {
    return keepAlive;
  }

  public Duration getMaxIdleTime() {
    return maxIdleTime;
  }

  public Duration getMaxLifeTime() {
    return maxLifeTime;
  }

  public Integer getMaxConnections() {
    return maxConnections;
  }

  public Duration getPendingAcquireTimeout() {
    return pendingAcquireTimeout;
  }

  public Duration getEvictInBackground() {
    return evictInBackground;
  }

  public boolean isEmpty() {
    return super.isEmpty() && readTimeoutMillis == null && writeTimeoutMillis == null;
  }

  public static class Builder extends ReactiveOptions.Builder{
    private Long readTimeoutMillis;
    private Long writeTimeoutMillis;
    private boolean keepAlive;
    private Duration maxIdleTime = Duration.ofMillis(-1);
    private Duration maxLifeTime = Duration.ofMillis(-1);
    private Integer maxConnections = 500;
    private Duration pendingAcquireTimeout = Duration.ofSeconds(45);
    private Duration evictInBackground;

    public Builder() {}

    public Builder setReadTimeoutMillis(long readTimeoutMillis) {
      this.readTimeoutMillis = readTimeoutMillis;
      return this;
    }

    public Builder setWriteTimeoutMillis(long writeTimeoutMillis) {
      this.writeTimeoutMillis = writeTimeoutMillis;
      return this;
    }

    public Builder setKeepAlive(boolean keepAlive) {
      this.keepAlive = keepAlive;
      return this;
    }

    public Builder setMaxIdleTime(Duration maxIdleTime) {
      this.maxIdleTime = maxIdleTime;
      return this;
    }

    public Builder setMaxLifeTime(Duration maxLifeTime) {
      this.maxLifeTime = maxLifeTime;
      return this;
    }

    public Builder setMaxConnections(Integer maxConnections) {
      this.maxConnections = maxConnections;
      return this;
    }

    public Builder setPendingAcquireTimeout(Duration pendingAcquireTimeout) {
      this.pendingAcquireTimeout = pendingAcquireTimeout;
      return this;
    }

    public Builder setEvictInBackground(Duration evictInBackground) {
      this.evictInBackground = evictInBackground;
      return this;
    }

    public WebReactiveOptions build() {
      return new WebReactiveOptions(useHttp2, connectTimeoutMillis, acceptCompressed, readTimeoutMillis,
              writeTimeoutMillis, keepAlive, maxIdleTime, maxLifeTime, maxConnections, pendingAcquireTimeout,
              evictInBackground);
    }
  }
}
