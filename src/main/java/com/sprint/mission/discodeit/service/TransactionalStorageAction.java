package com.sprint.mission.discodeit.service;

@FunctionalInterface
public interface TransactionalStorageAction {
    void execute() throws Exception;
}
