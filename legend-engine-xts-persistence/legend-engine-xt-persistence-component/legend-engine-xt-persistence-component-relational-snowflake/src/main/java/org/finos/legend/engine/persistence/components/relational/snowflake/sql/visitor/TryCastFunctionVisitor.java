// Copyright 2024 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.finos.legend.engine.persistence.components.logicalplan.values.TryCastFunction;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.snowflake.sql.SnowflakeDataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.DataType;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.ArrayList;
import java.util.List;

public class TryCastFunctionVisitor implements LogicalPlanVisitor<TryCastFunction>
{
    @Override
    public VisitorResult visit(PhysicalPlanNode prev, TryCastFunction current, VisitorContext context)
    {
        DataType dataType = new SnowflakeDataTypeMapping().getDataType(current.type());

        org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.values.TryCastFunction tryCastFunction =
            new org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.values.TryCastFunction(dataType, context.quoteIdentifier());
        for (Optimizer optimizer : context.optimizers())
        {
            tryCastFunction = (org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.values.TryCastFunction) optimizer.optimize(tryCastFunction);
        }
        prev.push(tryCastFunction);

        List<LogicalPlanNode> logicalPlanNodeList = new ArrayList<>();
        logicalPlanNodeList.add(current.field());

        return new VisitorResult(tryCastFunction, logicalPlanNodeList);
    }
}
