// Copyright 2023 Goldman Sachs
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

import org.finos.legend.engine.persistence.components.common.FileFormatType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.values.StagedFilesFieldValue;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.values.StagedFilesField;
import org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.values.ToDateFunction;
import org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.values.ToTimeFunction;
import org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.values.ToTimestampFunction;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.FunctionName;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Function;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.Arrays;
import java.util.Optional;

public class StagedFilesFieldValueVisitor implements LogicalPlanVisitor<StagedFilesFieldValue>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, StagedFilesFieldValue current, VisitorContext context)
    {
        StagedFilesField stageField = new StagedFilesField(context.quoteIdentifier(), current.columnNumber());
        current.elementPath().ifPresent(stageField::setElementPath);
        current.alias().ifPresent(stageField::setAlias);
        current.datasetRefAlias().ifPresent(stageField::setDatasetReferenceAlias);

        if (current.fieldType().dataType().equals(DataType.VARIANT) || current.fieldType().dataType().equals(DataType.JSON))
        {
           Function parseJson = new Function(FunctionName.fromName("PARSE_JSON"), Arrays.asList(stageField), null, context.quoteIdentifier());
           Function toVariant = new Function(FunctionName.fromName("TO_VARIANT"), Arrays.asList(parseJson), current.alias().orElse(null), context.quoteIdentifier());
           prev.push(toVariant);
           return new VisitorResult(null);
        }

        if (current.fileFormatType().isPresent()
                && current.fileFormatType().get().equals(FileFormatType.AVRO)
                && !current.disableAvroLogicalType().orElse(Boolean.FALSE))
        {
            if (current.fieldType().dataType().equals(DataType.TIMESTAMP))
            {
                int timestampScale = getTimestampScale(current);
                prev.push(new ToTimestampFunction(stageField, context.quoteIdentifier(), timestampScale));
                return new VisitorResult(null);
            }
            if (current.fieldType().dataType().equals(DataType.DATE))
            {
                prev.push(new ToDateFunction(stageField, context.quoteIdentifier()));
                return new VisitorResult(null);
            }
            if (current.fieldType().dataType().equals(DataType.TIME))
            {
                prev.push(new ToTimeFunction(stageField, context.quoteIdentifier()));
                return new VisitorResult(null);
            }
        }
        prev.push(stageField);
        return new VisitorResult(null);
    }

    private static int getTimestampScale(StagedFilesFieldValue current)
    {
        Optional<Integer> length = current.fieldType().length();
        if (length.isPresent())
        {
            return length.get();
        }

        Optional<Integer> scale = current.fieldType().scale();
        if (scale.isPresent())
        {
            return scale.get();
        }

        return 3; // Default is millis
    }


}
