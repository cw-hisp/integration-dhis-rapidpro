/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.integration.rapidpro.processor;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.hisp.dhis.api.model.v2_36_11.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContactProcessorUtils
{

    protected static final Logger LOGGER = LoggerFactory.getLogger( ContactProcessorUtils.class );

    public interface Callback<T>
    {
        void call( T obj );
    }

    @SuppressWarnings( "unchecked" )
    public static void mapContacts( Exchange exchange, Callback<Map.Entry<User, String>> onFound,
        Callback<User> ifNotFound )
    {
        List<User> dhis2Users = exchange.getProperty( "dhis2Users", List.class );

        Map<String, Object> rapidProContacts = exchange.getProperty( "rapidProContacts", Map.class );
        List<Map<String, Object>> results = (List<Map<String, Object>>) rapidProContacts.get( "results" );

        Map<String, Map<String, Object>> resultsInvMap = results.stream().collect( Collectors.toMap(
            c -> ((Map<String, Object>) c.get( "fields" )).get( "dhis2_user_id" ).toString(),
            c -> c ) );

        for ( User dhis2User : dhis2Users )
        {
            if ( dhis2User.getId().isPresent() )
            {
                if ( resultsInvMap.containsKey( dhis2User.getId().get() ) )
                {
                    onFound.call( new AbstractMap.SimpleEntry<>( dhis2User,
                        (String) resultsInvMap.get( dhis2User.getId().get() ).get( "uuid" ) ) );
                }
                else
                {
                    ifNotFound.call( dhis2User );
                }
            }
            else
            {
                LOGGER.warn( "Found a DHIS2 user without the presence of an ID : " + dhis2User );
            }
        }

    }
}
