package moonrise.pjt1.party.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import moonrise.pjt1.commons.response.ResponseDto;
import moonrise.pjt1.member.entity.Member;
import moonrise.pjt1.member.repository.MemberRepository;
import moonrise.pjt1.movie.entity.Movie;
import moonrise.pjt1.movie.repository.MovieRepository;
import moonrise.pjt1.party.dto.*;
import moonrise.pjt1.party.entity.*;
import moonrise.pjt1.party.repository.PartyCommentRepository;
import moonrise.pjt1.party.repository.PartyInfoRepository;
import moonrise.pjt1.party.repository.PartyJoinRepository;
import moonrise.pjt1.party.repository.PartyRepository;
import moonrise.pjt1.util.HttpUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Log4j2
public class PartyService {
    private final PartyRepository partyRepository;
    private final PartyCommentRepository partyCommentRepository;
    private final MovieRepository movieRepository;
    private final MemberRepository memberRepository;
    private final PartyJoinRepository partyJoinRepository;
    private final PartyInfoRepository partyInfoRepository;
    private final RedisTemplate redisTemplate;
    public Map<String,Object> readParty(Long partyId) {
        String key = "partyViewCnt::"+partyId;
        //캐시에 값이 없으면 레포지토리에서 조회 있으면 값을 증가시킨다.
        ValueOperations valueOperations = redisTemplate.opsForValue();
        if(valueOperations.get(key)==null){
            valueOperations.set(
                    key,
                    String.valueOf(partyInfoRepository.findPartyViewCnt(partyId)+1),
                    20,
                    TimeUnit.MINUTES);
        }
        else{
            valueOperations.increment(key);
        }
        int viewCnt = Integer.parseInt((String) valueOperations.get(key));
        log.info("value:{}",viewCnt);
        Optional<Party> findParty = partyRepository.findById(partyId);
        Party party = findParty.get();
        List<PartyComment> partyComments = partyCommentRepository.getCommentList(partyId);
        List<PartyJoin> partyJoins = party.getPartyJoins();
        Map<String,Object> result = new HashMap<>();
        if(findParty.isPresent()){
            PartyReadResponseDto partyReadResponseDto = new PartyReadResponseDto(party.getId(),party.getTitle(),party.getContent(),party.getPartyDate(),
                    party.getPartyPeople(),party.getLocation(),party.getPartyStatus(),party.getMember().getId(),
                    party.getMovie().getId(),partyJoins,partyComments,party.getDeadLine(), viewCnt);
            result.put("findParty",partyReadResponseDto);
        }
        return result;
    }
//    public Map<String,Object> listParty(Long movieId) {
//        Map<String,Object> result = new HashMap<>();
//        List<Party> partyList = partyRepository.findPartyList(movieId);
//        List<PartyListResponseDto> partyListResponseDtos = new ArrayList<>();
//        for (Party party : partyList) {
//            PartyListResponseDto partyListResponseDto = new PartyListResponseDto(
//                party.getId(),party.getTitle(),party.getPartyPeople(),party.getLocation(),
//                    party.getPartyDate(),party.getPartyInfo().getViewCnt()
//            );
//            partyListResponseDtos.add(partyListResponseDto);
//        }
//        result.put("findParties",partyListResponseDtos);
//        return result;
//    }
    public Map<String,Object> listParty(Long movieId, PageRequest pageable) {
        Map<String,Object> result = new HashMap<>();
        Page<Party> partyList = partyRepository.findPartyList(movieId, pageable);

        result.put("findParties",partyList.get());
        result.put("totalPages", partyList.getTotalPages());
        return result;
    }
    public ResponseDto createParty(String access_token, PartyCreateDto partyCreateDto) {
        Map<String, Object> result = new HashMap<>();

        // token parsing 요청
        Long user_id = HttpUtil.requestParingToken(access_token);
        ResponseDto responseDto = new ResponseDto();

        if(user_id == 0L){
            responseDto.setStatus_code(400);
            responseDto.setMessage("회원 정보가 없습니다.");
            return responseDto;
        }
        // member_id 매핑
        partyCreateDto.setMemberId(user_id);

        //DB
        Optional<Member> findMember = memberRepository.findById(partyCreateDto.getMemberId());
        Optional<Movie> findMovie = movieRepository.findById(partyCreateDto.getMovieId());
        PartyInfo partyInfo = new PartyInfo();
        Party party = Party.createParty(partyCreateDto, findMember.get(), findMovie.get(),partyInfo);
        partyRepository.save(party);

        //responseDto 작성
        result.put("party_id",party.getId());
        responseDto.setMessage("소모임 작성 완료");
        responseDto.setData(result);
        responseDto.setStatus_code(200);

        return responseDto;
    }
    @Transactional
    public Party modifyParty(PartyModifyDto partyModifyDto) {
        Party party = partyRepository.findById(partyModifyDto.getPartyId()).get();
        party.modifyParty(partyModifyDto);
        return party;
    }
    @Transactional
    public Long createComment(PartyCommentCreateDto partyCommentCreateDto) {
        Optional<Member> findMember = memberRepository.findById(partyCommentCreateDto.getMemberId());
        Optional<Party> findParty = partyRepository.findById(partyCommentCreateDto.getPartyId());

        PartyComment partyComment = PartyComment.createPartyComment(partyCommentCreateDto, findParty.get(), findMember.get());
        partyCommentRepository.save(partyComment);
        Long commentId = partyComment.getId();
        // 원댓글이면 groupId 를 본인 pk 로 저장
        if (partyComment.getGroupId() == 0L){
            partyComment.setGroupId(commentId);
        }

        return partyComment.getId();
    }

    public Long createJoin(PartyJoinCreateDto partyJoinCreateDto) {
        Optional<Member> findMember = memberRepository.findById(partyJoinCreateDto.getMemberId());
        Optional<Party> findParty = partyRepository.findById(partyJoinCreateDto.getPartyId());

        PartyJoin partyJoin = PartyJoin.createPartyJoin(partyJoinCreateDto,findMember.get(),findParty.get());
        partyJoinRepository.save(partyJoin);

        return partyJoin.getId();
    }
    @Transactional
    public void updatePartyStatus(Long partyId, int status) {
        Party party = partyRepository.findById(partyId).get();
        if(status == 1){
            party.setPartyStatus(PartyStatus.모집완료);
        }
        else if(status == 2){
            party.setPartyStatus(PartyStatus.기간만료);
        }
        else if(status == 3){
            party.setPartyStatus(PartyStatus.삭제);
        }
    }
    @Transactional
    public void updateJoinStatus(Long joinId, int status) {
        PartyJoin partyJoin = partyJoinRepository.findById(joinId).get();
        if(status == 1){
            partyJoin.setPartyJoinStatus(PartyJoinStatus.승인);
        }
        else if(status == 2){
            partyJoin.setPartyJoinStatus(PartyJoinStatus.거절);
        }
        else if(status == 3){
            partyJoin.setPartyJoinStatus(PartyJoinStatus.취소);
        }
    }
    @Transactional
    public Long updateComment(PartyCommentUpdateDto partyCommentUpdateDto) {
        Long commentId = partyCommentUpdateDto.getCommentId();
        Optional<PartyComment> findComment = partyCommentRepository.findById(commentId);
        if(!findComment.isPresent()){
            throw new IllegalStateException("수정할 댓글을 찾을 수 없습니다.");
        }
        PartyComment partyComment = findComment.get();
        partyComment.setContent(partyCommentUpdateDto.getContent());
        partyComment.setShowPublic(partyCommentUpdateDto.isShowPublic());
        partyComment.setCommentWriteTime(LocalDateTime.now());
        return partyComment.getId();
    }
    @Transactional
    public void statusComment(Long commentId, int statusCode) {
        Optional<PartyComment> findComment = partyCommentRepository.findById(commentId);
        if(!findComment.isPresent()){
            throw new IllegalStateException("존재하지 않는 댓글입니다.");
        }
        PartyComment partyComment = findComment.get();
        switch (statusCode) {
            case 1:
                partyComment.normalize();
                break;
            case 2:
                partyComment.banned();
                break;
            case 3:
                partyComment.deleted();
                break;
        }
    }
}