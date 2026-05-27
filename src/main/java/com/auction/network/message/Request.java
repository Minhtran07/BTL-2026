package com.auction.network.message;

import java.util.UUID;

public abstract class Request extends Message {
  private static final long serialVersionUID = 2L;

  private final String id;

  protected Request() {
    this.id = UUID.randomUUID().toString();
  }

  @Override
  public String getId() { return id; }
}