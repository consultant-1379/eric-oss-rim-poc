/*******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/
package com.ericsson.oss.apps.model.mom;

import com.ericsson.oss.apps.model.Constants;
import com.opencsv.bean.CsvBindByName;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;

import static com.ericsson.oss.apps.model.mom.MoType.MANAGED_ELEMENT;

@Getter
@ToString
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ManagedObjectId implements Serializable {

    private static final long serialVersionUID = 1;

    @CsvBindByName
    @Column(name = "me_fdn")
    private String meFdn;
    @CsvBindByName
    @Column(name = "ref_id")
    private String refId;

    public ManagedObjectId(String fdn) {
        String[] fdnParts = splitFdn(fdn);
        meFdn = fdnParts[0].intern();
        refId = fdnParts[1];
    }

    public static ManagedObjectId of(String fdn) {
        return new ManagedObjectId(fdn);
    }

    public static ManagedObjectId of(String meFdn, String refId) {
        return new ManagedObjectId(meFdn, refId);
    }

    private static String[] splitFdn(String fdn) {
        int i = fdn.indexOf(Constants.COMMA, fdn.indexOf(MANAGED_ELEMENT.toString()));
        if (i > 0) {
            return new String[]{fdn.substring(0, i), fdn.substring(i+1)};
        } else {
            return new String[]{fdn, Constants.EMPTY};
        }
    }

    public ManagedObjectId fetchParentId() {
        String parentHref = refId.substring(0, Math.max(0, refId.lastIndexOf(Constants.COMMA)));
        return new ManagedObjectId(meFdn, parentHref);
    }

    public ManagedObjectId fetchMEId() {
        return new ManagedObjectId(meFdn, Constants.EMPTY);
    }

    public String fetchDNValue() {
        return refId.substring(refId.lastIndexOf(Constants.EQUAL) + 1);
    }

    public MoType findMoType() {
        return MoType.getMoType(refId);
    }

    //Todo: @Deprecated
    public String toFdn() {
        return meFdn + Constants.COMMA + refId;
    }

    @Override
    public String toString() { return toFdn(); }
}
