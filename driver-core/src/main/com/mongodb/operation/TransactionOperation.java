/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.operation;

import com.mongodb.WriteConcern;
import com.mongodb.binding.ReadBinding;
import com.mongodb.connection.Connection;
import com.mongodb.operation.OperationHelper.CallableWithConnection;
import org.bson.BsonDocument;
import org.bson.BsonInt32;

import static com.mongodb.assertions.Assertions.isTrue;
import static com.mongodb.assertions.Assertions.notNull;
import static com.mongodb.operation.CommandOperationHelper.executeWrappedCommandProtocol;
import static com.mongodb.operation.OperationHelper.withConnection;
import static com.mongodb.operation.WriteConcernHelper.writeConcernErrorTransformer;

/**
 * A base class for transaction-related operations
 *
 * @since 3.8
 */
public abstract class TransactionOperation implements ReadOperation<Void> {
    private final WriteConcern writeConcern;

    /**
     * Construct an instance.
     *
     * @param writeConcern the write concern
     */
    protected TransactionOperation(final WriteConcern writeConcern) {
        this.writeConcern = notNull("writeConcern", writeConcern);
    }

    /**
     * Gets the write concern.
     *
     * @return the write concern
     */
    public WriteConcern getWriteConcern() {
        return writeConcern;
    }

    @Override
    public Void execute(final ReadBinding binding) {
        isTrue("in transaction", binding.getSessionContext().hasActiveTransaction());
        return withConnection(binding, new CallableWithConnection<Void>() {
            @Override
            public Void call(final Connection connection) {
                executeWrappedCommandProtocol(binding, "admin", getCommand(binding), connection, writeConcernErrorTransformer());
                return null;
            }
        });
    }

    private BsonDocument getCommand(final ReadBinding binding) {
        BsonDocument command = new BsonDocument(getCommandName(), new BsonInt32(1));
        if (!getWriteConcern().isServerDefault()) {
            command.put("writeConcern", getWriteConcern().asDocument());
        }
        return command;
    }

    /**
     * Gets the command name.
     *
     * @return the command name
     */
    protected abstract String getCommandName();
}
