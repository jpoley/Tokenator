package org.tokenator.opentokenizer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.tokenator.opentokenizer.domain.entity.PrimaryData;
import org.tokenator.opentokenizer.domain.entity.SurrogateData;
import org.tokenator.opentokenizer.domain.repository.PrimaryDataRepository;
import org.tokenator.opentokenizer.domain.repository.SurrogateDataRepository;

import javax.transaction.Transactional;
import java.util.Date;

import static org.tokenator.opentokenizer.util.DateSerializer.DATE_FORMAT;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private PrimaryDataRepository primaryDataRepo;
    private SurrogateDataRepository surrogateDataRepo;

    @Autowired
    public ApiController(
            PrimaryDataRepository primaryDataRepo,
            SurrogateDataRepository surrogateDataRepo
    ) {
        this.primaryDataRepo = primaryDataRepo;
        this.surrogateDataRepo = surrogateDataRepo;
    }


   /*
    *  Create a primary data entry.  Example:
    *
    *   $ curl -X POST -H 'Content-Type: application/json' -d '{"pan": "4046460664629718", "expr": "1801"}' \
    *       http://localhost:8080/api/v1/primaries/
    */
    @RequestMapping(
            value = "/primaries",
            method = RequestMethod.POST
    )
    @ResponseStatus(HttpStatus.CREATED)
    public PrimaryData createPrimary(@RequestBody PrimaryData primaryData) {
        return primaryDataRepo.save(primaryData);
    }


   /*
    *  Lookup primary PAN data by id. Example retrieving primary data for id=1:
    *
    *   $ curl -X GET http://localhost:8080/api/v1/primaries/1
    */
    @RequestMapping(
            value = "/primaries/{primaryId}",
            method = RequestMethod.GET
    )
    @ResponseStatus(HttpStatus.CREATED)
    public PrimaryData findPrimaryById(@PathVariable(value="primaryId") Long primaryId) {
        PrimaryData primary = primaryDataRepo.findOne(primaryId);
        if (primary == null) {
            throw new EntityNotFoundException(PrimaryData.class, primaryId);
        }
        return primary;
    }


    /*
     *  Lookup primary data by pan and yyMM expiration date.  Example:
     *
     *   $ curl -X GET http://localhost:8080/api/v1/primaries/4046460664629718/1801
     */
    @RequestMapping(
            value = "/primaries/{primaryPan}/{primaryExpr}",
            method = RequestMethod.GET
    )
    @ResponseStatus(HttpStatus.OK)
    public PrimaryData findPrimaryByPanAndExpr(
            @PathVariable(value="primaryPan") String primaryPan,
            @PathVariable(value="primaryExpr")
            @DateTimeFormat(pattern = DATE_FORMAT) Date primaryExpr) {

        PrimaryData primary = primaryDataRepo.findByPanAndExpr(primaryPan, primaryExpr);
        if (primary == null) {
            throw new EntityNotFoundException(PrimaryData.class, primaryPan, primaryExpr);
        }
        return primary;
    }

    /*
     *  Delete primary PAN data by id.  Surrogates are deleted by cascade. Example
     *
     *   $ curl -X DELETE http://localhost:8080/api/v1/primaries/1
     */
    @RequestMapping(
            value = "/primaries/{primaryId}",
            method = RequestMethod.DELETE
    )
    @ResponseStatus(HttpStatus.NO_CONTENT) /* 204, success but no response payload */
    public void deletePrimaryById(@PathVariable(value="primaryId") long primaryId) {
        primaryDataRepo.delete(primaryId);
    }



    /*
     *  Create a surrogate for a primary data entry with the specified id.  Example:
     *
     * $ curl -X POST -H 'Content-Type: application/json' -d '{"pan": "98765432109876", "expr": "1801"}' \
     *     http://localhost:8080/api/v1/primaries/1/surrogates/
     */
    @RequestMapping(
            value = "/primaries/{primaryId}/surrogates/",
            method = RequestMethod.POST
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public PrimaryData createSurrogate(
            @PathVariable(value="primaryId") Long primaryId,
            @RequestBody SurrogateData surrogateData) {

        PrimaryData primary = primaryDataRepo.findOne(primaryId);
        if (primary == null) {
            throw new EntityNotFoundException(PrimaryData.class, primaryId);
        }

        primary.addSurrogate(surrogateData);

        return primary;
    }


    /*
     *  Find the primary data that owns the requested surrogate pan+expr. Example:
     *
     *   $ curl -X GET http://localhost:8080/api/v1/primaries/98765432109876/1801
     */
    @RequestMapping(
            value = "/primaries/surrogates/{surrogatePan}/{surrogateExpr}",
            method = RequestMethod.GET
    )
    @ResponseStatus(HttpStatus.OK)
    public PrimaryData findPrimaryOfSurrogate(
            @PathVariable(value="surrogatePan") String surrogatePan,
            @PathVariable(value="surrogateExpr")
            @DateTimeFormat(pattern = DATE_FORMAT) Date surrogateExpr) {
        PrimaryData primary = primaryDataRepo.findBySurrogate(surrogatePan, surrogateExpr);
        if (primary == null) {
            throw new EntityNotFoundException(SurrogateData.class, surrogatePan, surrogateExpr);
        }
        return primary;
    }


   /*
    *  Delete surrogate PAN data by id. Example
    *
    *   $ curl -X DELETE http://localhost:8080/api/v1/surrogates/1
    */
    @RequestMapping(
            value = "/surrogates/{surrogateId}",
            method = RequestMethod.DELETE
    )
    @ResponseStatus(HttpStatus.NO_CONTENT) /* 204, success but no response payload */
    @Transactional
    public void deleteSurrogate(@PathVariable(value="surrogateId") long surrogateId) {
        surrogateDataRepo.delete(surrogateId);
    }


    @ExceptionHandler(EmptyResultDataAccessException.class)
    @ResponseStatus(value=HttpStatus.NOT_FOUND,reason="Entity not found")
    public void notFound() {
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(value=HttpStatus.CONFLICT,reason="Entity already exists")
    public void duplicateEntryExists() {
    }

}
