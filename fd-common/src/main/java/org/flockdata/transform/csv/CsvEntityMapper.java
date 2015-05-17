/*
 * Copyright (c) 2012-2015 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.transform.csv;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.flockdata.helper.FlockException;
import org.flockdata.profile.ImportProfile;
import org.flockdata.profile.model.ProfileConfiguration;
import org.flockdata.registration.bean.TagInputBean;
import org.flockdata.track.bean.EntityInputBean;
import org.flockdata.track.model.EntityKey;
import org.flockdata.transform.ColumnDefinition;
import org.flockdata.transform.DelimitedMappable;
import org.flockdata.transform.TransformationHelper;
import org.flockdata.transform.tags.TagProfile;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * User: mike
 * Date: 27/04/14
 * Time: 4:34 PM
 */
public class CsvEntityMapper extends EntityInputBean implements DelimitedMappable {

    private Logger logger = LoggerFactory.getLogger(CsvEntityMapper.class);

    public CsvEntityMapper(ImportProfile importProfile) {
        setDocumentName(importProfile.getDocumentName());
        setFortress(importProfile.getFortressName());
        setFortressUser(importProfile.getFortressUser());
    }

    @Override
    public ProfileConfiguration.ContentType getImporter() {
        return ProfileConfiguration.ContentType.CSV;
    }

    @Override
    public Map<String, Object> setData(final String[] headerRow, final String[] line, ProfileConfiguration importProfile) throws JsonProcessingException, FlockException {
        //Map<String, Object> row = toMap(importProfile, headerRow, line);
        setArchiveTags(importProfile.isArchiveTags());
        Map<String, Object> row = TransformationHelper.convertToMap(importProfile, headerRow, line);
        Map<String, ColumnDefinition> content = importProfile.getContent();

        for (String sourceColumn : content.keySet()) {
            sourceColumn = sourceColumn.trim();
            ColumnDefinition colDef = importProfile.getColumnDef(sourceColumn);

            if (colDef != null) {
                // Import Profile let's you alter the name of the column
                String valueColumn = (colDef.getTarget() == null ? sourceColumn : colDef.getTarget());
                Object o = row.get(valueColumn);
                String value = null;
                if (o != null)
                    value = o.toString().trim();

                if (colDef.isDescription()) {

                    setDescription(TransformationHelper.getValue(row, colDef.getValue(), colDef, value));
                }
                if (colDef.isCreateDate()) {
                    Long millis =TransformationHelper.parseDate(colDef, value);
                    if ( millis !=null)
                        setWhen(new DateTime(millis));
                }

                if (colDef.isCallerRef()) {
                    String callerRef = TransformationHelper.getValue(row, ColumnDefinition.ExpressionType.CALLER_REF, colDef, value);
                    setCallerRef(callerRef);
                }
                if (colDef.getDelimiter() != null) {
                    // Implies a tag because it is a comma delimited list of values
                    if (value != null && !value.equals("")) {
                        TagProfile tagProfile = new TagProfile();
                        tagProfile.setLabel(colDef.getLabel());
                        tagProfile.setReverse(colDef.getReverse());
                        tagProfile.setMustExist(colDef.isMustExist());
                        tagProfile.setCode(sourceColumn);
                        tagProfile.setDelimiter(colDef.getDelimiter());
                        String relationship = TransformationHelper.getRelationshipName(row, colDef);
                        Collection<TagInputBean> tags = TransformationHelper.getTagsFromList(tagProfile, row, relationship);
                        for (TagInputBean tag : tags) {
                            addTag(tag);
                        }

                    }
                } else if (colDef.isTag()) {
                    TagInputBean tag = new TagInputBean();

                    if (TransformationHelper.getTagInputBean(tag, row, sourceColumn, importProfile.getContent(), value)) {
                        addTag(tag);


                    }
                }
                if (colDef.isTitle()) {
                    setName(value);
                }
                if (colDef.isCreateUser()) { // The user in the calling system
                    setFortressUser(value);
                }

                if (colDef.isUpdateUser()) {
                    setUpdateUser(value);
                }
                if (!colDef.getCrossReferences().isEmpty()) {
                    for (Map<String, String> key : colDef.getCrossReferences()) {
                        addCrossReference(key.get("relationshipName"), new EntityKey(key.get("fortress"), key.get("documentName"), value));
                    }
                }

                if (colDef.hasEntityProperies()) {
                    for (ColumnDefinition columnDefinition : colDef.getProperties()) {
                        //String sourceCol = columnDefinition.getSource();
                        if (columnDefinition.isPersistent()) {

                            value = TransformationHelper.getValue(row, columnDefinition.getValue(), columnDefinition, row.get(valueColumn));
                            Object oValue = TransformationHelper.getValue(value, columnDefinition);
                            if (columnDefinition.getTarget() != null)
                                valueColumn = columnDefinition.getTarget();
                            if ( oValue != null)
                                setProperty(valueColumn, oValue);
                        }

                    }
                }

            } // ignoreMe
        }
        Collection<String> strategyCols = importProfile.getStrategyCols();
        for (String strategyCol : strategyCols) {
            ColumnDefinition colDef = importProfile.getColumnDef(strategyCol);
            logger.error("This routine has no test and has not been figured out");
            // ToDo: Figure this out
            //String callerRef = dataResolver.resolve(strategyCol, getColumnValues(colDef, row));

            //if (callerRef != null) {
//                addCrossReference(colDef.getStrategy(), new EntityKey(colDef.getFortress(), colDef.getDocumentType(), callerRef));
//            }
        }

        if (importProfile.getEntityKey() != null) {
            ColumnDefinition columnDefinition = importProfile.getColumnDef(importProfile.getEntityKey());
            if (columnDefinition != null) {
                String[] dataCols = columnDefinition.getRefColumns();
                String callerRef = "";
                for (String dataCol : dataCols) {
                    callerRef = callerRef + (!callerRef.equals("") ? "." : "") + row.get(dataCol);
                }
                setCallerRef(callerRef);
            }

        }

        return row;
    }

    @Override
    public boolean hasHeader() {
        return true;
    }

    public static DelimitedMappable newInstance(ImportProfile importProfile) {
        return new CsvEntityMapper(importProfile);
    }

    @Override
    public char getDelimiter() {
        return ',';
    }

}
